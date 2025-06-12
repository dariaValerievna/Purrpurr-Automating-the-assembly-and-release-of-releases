const API_URL = 'http://localhost:8080'; //URL будет добавлен позже


export async function getProjects() {
  const token = localStorage.getItem('mockAuthToken'); 

  const response = await fetch(`${API_URL}/api/releases/gitlab`, {
    headers: {
      'Content-Type': 'application/json',
      //'Authorization': `Bearer ${token}`, 
    },
  });

  if (!response.ok) {
    throw new Error('Ошибка при получении проектов');
  }

  const data = await response.json();
  return data;
}

export async function getTasks() {
  const token = localStorage.getItem('mockAuthToken'); 

  const response = await fetch(API_URL/gettasks, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`, 
    },
  });

  if (!response.ok) {
    throw new Error('Ошибка при получении задач');
  }

  const data = await response.json();
  return data;
}

export async function getBranches() {
  const token = localStorage.getItem('mockAuthToken'); 

  const response = await fetch(API_URL/getbranches, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`, 
    },
  });

  if (!response.ok) {
    throw new Error('Ошибка при получении веток');
  }

  const data = await response.json();
  return data;
}
